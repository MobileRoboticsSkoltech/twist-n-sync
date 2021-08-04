//
// Created by achains on 18.07.2021.
//

#include "TimeSync.h"
#include "util/TSUtil.h"
#include "util/CubicSpline.h"

#include "Eigen/Dense"

#include <numeric>
#include <memory>


TimeSync::TimeSync(std::vector<std::vector<double>> const & gyro_first,
                   std::vector<std::vector<double>> const & gyro_second,
                   std::vector<double> const & ts_first,
                   std::vector<double> const & ts_second,
                   bool const & do_resample):
                        gyro_first_(TSUtil::vectorToEigMatrixX3d(gyro_first)),
                        gyro_second_(TSUtil::vectorToEigMatrixX3d(gyro_second)),
                        ts_first_(TSUtil::vectorToEigVectorXd(ts_first)),
                        ts_second_(TSUtil::vectorToEigVectorXd(ts_second)),
                        do_resample_(do_resample) {}


TSUtil::CorrData TimeSync::getInitialIndex() const {

    Eigen::VectorXd norm_first = TSUtil::getNormOfRows(gyro_first_);
    Eigen::VectorXd norm_second = TSUtil::getNormOfRows(gyro_second_);

    Eigen::VectorXd cross_cor = TSUtil::eigenCrossCor(norm_first, norm_second);

    return {cross_cor, std::distance(cross_cor.begin(), std::max_element(cross_cor.begin(), cross_cor.end()))};
}

Eigen::MatrixX3d & TimeSync::interpolateGyro(Eigen::VectorXd const & ts_old, Eigen::MatrixX3d const & gyro_old,
                                           Eigen::VectorXd const & ts_new, Eigen::MatrixX3d & gyro_new) {
    assert (gyro_old.rows() == gyro_new.rows());

    gyro_new << TSUtil::interpolate(ts_old, gyro_old(Eigen::all, 0), ts_new),
                TSUtil::interpolate(ts_old, gyro_old(Eigen::all, 1), ts_new),
                TSUtil::interpolate(ts_old, gyro_old(Eigen::all, 2), ts_new);
    return gyro_new;
}

void TimeSync::resample(double const & accuracy){
    double time_first_mean = TSUtil::adjDiffEigen(ts_first_).mean();
    double time_second_mean = TSUtil::adjDiffEigen(ts_second_).mean();

    dt_ = std::min({accuracy, time_first_mean, time_second_mean});

    if (do_resample_){
        Eigen::VectorXd ts_first_new = TSUtil::arangeEigen(ts_first_[0], *ts_first_.end() + dt_, dt_);
        Eigen::VectorXd ts_second_new = TSUtil::arangeEigen(ts_second_[0], *ts_second_.end() + dt_, dt_);

        TimeSync::interpolateGyro(ts_first_, gyro_first_, ts_first_new, gyro_first_);
        TimeSync::interpolateGyro(ts_second_, gyro_second_, ts_second_new, gyro_second_);
    }
}

Eigen::Vector2d TimeSync::obtainRoots(Eigen::VectorXd const & coeffs, Eigen::Index const & order){
    Eigen::VectorXd equation(order);
    for (auto i = 0; i < order; ++i){
        equation[i] = static_cast<double>(order - i) * coeffs[i];
    }
    return TSUtil::quadraticRoots(equation.reverse());
}

void TimeSync::obtainDelay(){

    // Correction of index numbering
    Eigen::Index shift = -gyro_first_.rows() + 1;

    // Cross-cor estimation
    TSUtil::CorrData corr_data = TimeSync::getInitialIndex();
    corr_data.initial_index += shift;

    Eigen::MatrixX3d tmp_xx1;
    Eigen::MatrixX3d tmp_xx2;

    if (corr_data.initial_index > 0){
        tmp_xx1 = gyro_first_(Eigen::seq(0, Eigen::last - corr_data.initial_index), Eigen::all).eval();
        tmp_xx2 = gyro_second_(Eigen::seq(corr_data.initial_index, Eigen::last), Eigen::all).eval();
    }
    else if (corr_data.initial_index < 0){
        tmp_xx1 = gyro_first_(Eigen::seq(-corr_data.initial_index, Eigen::last), Eigen::all).eval();
        tmp_xx2 = gyro_second_(Eigen::seq(0, corr_data.initial_index), Eigen::all).eval();
    }
    else{
        tmp_xx1 = gyro_first_;
        tmp_xx2 = gyro_second_;
    }

    Eigen::Index size = std::min(tmp_xx1.rows(), tmp_xx2.rows());
    tmp_xx1 = tmp_xx1(Eigen::seq(0, size - 1), Eigen::all).eval();
    tmp_xx2 = tmp_xx2(Eigen::seq(0, size - 1), Eigen::all).eval();

    // Calibration
    Eigen::Matrix3d M = (tmp_xx2.transpose() * tmp_xx1) * (tmp_xx1.transpose() * tmp_xx1).inverse();

    gyro_first_ = (M * gyro_first_.transpose()).transpose().eval();

    // Cross-correlation re-estimation
    corr_data = TimeSync::getInitialIndex();

    // Cross-cor, based cubic spline coefficients
    CubicSpline cubic_spline(TSUtil::arangeEigen(0., static_cast<double>(corr_data.cross_cor.size())),
                                corr_data.cross_cor);

    Eigen::Matrix4Xd spline_coefficients = cubic_spline.getCoefficients();

    Eigen::VectorXd coeffs = spline_coefficients.col(corr_data.initial_index);

    // Check cubic spline derivative sign and redefine initial_index if needed
    if (coeffs(Eigen::last - 1) < 0) {
        corr_data.initial_index -= 1;
        coeffs = spline_coefficients(Eigen::all, corr_data.initial_index);
    }

    // Solve quadratic equation to obtain roots
    Eigen::Index order = coeffs.size() - 1;
    Eigen::Vector2d roots = TimeSync::obtainRoots(coeffs, order);

    auto result = *std::max_element(roots.begin(), roots.end());
    std::vector<double> check_solution(order);
    for (int i = 0; i < order; ++i)
        check_solution[i] = static_cast<double>(order - i) * coeffs[i] *
                std::pow((roots[0] + roots[1]) / 2, (order - i - 1));
    if (std::accumulate(check_solution.begin(), check_solution.end(), 0.0) < 0.0)
        result = *std::min_element(roots.begin(), roots.end());

    time_delay_ = (static_cast<double>(corr_data.initial_index + shift) + result) * dt_;
}

double TimeSync::getTimeDelay() const {
    return time_delay_;
}
